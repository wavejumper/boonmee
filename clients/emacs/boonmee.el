(require 'json)

(defvar boonmee-command "boonmee")

(defun boonmee-handle-error (resp)
  (print "boonmee: error"))

(defun boonmee-handle-definition (resp)
  (let* ((data (plist-get resp :data))
         (start (plist-get data :start))
         (line (plist-get start :line))
         (column (plist-get start :offset))
         (file (plist-get data :file)))
    (find-file file)
    (goto-line line)
    (move-to-column column)))

(defun boonmee-handle-quickinfo (resp)
  (let* ((data (plist-get resp :data))
         (display-string (plist-get data :displayString)))
    (message display-string)))

(defun boonmee-handle-response (process output)
  (let* ((json-object-type 'plist)
         (resp (json-read-from-string output))
         (command (plist-get resp :command))
         (success (plist-get resp :success)))
    (cond
     ((string= "error" command)
      (boonmee-handle-error resp))

     ((and (string= "definition" command) success)
      (boonmee-handle-definition resp))

     ((and (string= "quickinfo" command) success)
      (boonmee-handle-quickinfo resp))

     (not success)
     (print (concat "boonmee: command failed " output))

     (t (message (concat "boonmee: cannot handle command " command) )))))

(defun boonmee-init ()
  (if (get-process "boonmee")
      nil
    (let ((proc (start-process "boonmee" "boonmee-out" boonmee-command)))
      (set-process-filter proc 'boonmee-handle-response))))

(defun boonmee-project-root (file)
  (let ((dir (file-name-directory file)))
    (if (file-exists-p (concat dir "node_modules"))
        dir
      (project-root dir))))

(defun boonmee-goto-definition ()
  (interactive)
  (let* ((line (string-to-number (format-mode-line "%l")))
         (offset (string-to-number (format-mode-line "%c")))
         (file (buffer-file-name))
         ;; TODO: generate unique request id
         (req-id "1234")
         (args (list :file file :line line :offset offset :projectRoot (file-name-directory file)))
         (req (json-encode (list :command "definition" :type "request" :requestId req-id :arguments args))))
    (process-send-string "boonmee" (concat req "~\n"))))

(defun boonmee-quickinfo ()
  (interactive)
  (let* ((line (string-to-number (format-mode-line "%l")))
         (offset (string-to-number (format-mode-line "%c")))
         (file (buffer-file-name))
         ;; TODO: generate unique request id
         (req-id "1234")
         (args (list :file file :line line :offset offset :projectRoot (file-name-directory file)))
         (req (json-encode (list :command "quickinfo" :type "request" :requestId req-id :arguments args))))
    (process-send-string "boonmee" (concat req "~\n"))))
